const express = require('express');
const cors = require('cors'); // access our server from different domains
const mongoose = require('mongoose');
const ip = require('ip');
const path = require('path');
require('dotenv/config');
const schemas = require('./models/schemas');
const {AceBase} = require('acebase');

const app = express();

app.set('trust proxy', true);


const corsOptions = {
    origin: "*",
    credentials:true,
    optionSuccessStatus:200
}

app.use(cors(corsOptions));

app.use(express.json());

const acebase = new AceBase({dbname: 'mydb', host: 'localhost', port: 3000, https:true});

app.post('/api/shoppingList', async (req, res) => {
    const shoppingList = req.body;
    const ref = await acebase.ref('shoppingLists').push(shoppingList);
    res.json({id:ref.key});
})

app.get('/api/shoppingList/:id', async (req, res) => {
    const id = req.params.id;
    const ref = await acebase.ref(`shoppingLists/${id}`).once('value');
    const shoppingList = ref.val()
    res.json(shoppingList);
})

app.post('/api/shoppingList/:id/items', async (req, res) => {
    const item = req.body.item;
    const itemName = item.name;
    const itemQuantity = item.quantityDesired;

    const itemModelToSave = {
        name: itemName,
        quantityDesired: itemQuantity,
        quantityAcquired: 0
    }

    const ref = await acebase.ref('items').push(itemModelToSave);
    const itemId = ref.key;

    const shoppingListId = req.params.id;
    const shoppingListRef = await acebase.ref(`shoppingLists/${shoppingListId}`).once('value');
    const shoppingList = shoppingListRef.val();
    const shoppingListItems = shoppingList.items;

    const itemToPush = {
        id: itemId,
        name: itemName,
        quantityDesired: parseInt(itemQuantity),
        quantityAcquired: 0
    }

    shoppingListItems.push(itemToPush);
    shoppingList.items = shoppingListItems;
    await acebase.ref(`shoppingLists/${shoppingListId}`).set(shoppingList);
    res.json(shoppingList);
});

app.delete('/api/shoppingList/:id/items/:itemId', async (req, res) => {
    const itemId = req.params.itemId;
    const shoppingListId = req.params.id;

    const shoppingListRef = await acebase.ref(`shoppingLists/${shoppingListId}`).once('value');
    const shoppingList = shoppingListRef.val();
    const shoppingListItems = shoppingList.items;

    const newShoppingListItems = shoppingListItems.filter(item => item.id !== itemId);
    shoppingList.items = newShoppingListItems;
    await acebase.ref(`shoppingLists/${shoppingListId}`).set(shoppingList);
    res.json(shoppingList);
});


const dbOptions = {useNewUrlParser: true, useUnifiedTopology: true};
mongoose.connect(process.env.DB_URI, dbOptions)
.then(() => console.log('Connected to DB'))
.catch(err => console.log(err));


const port = process.env.PORT || 4000;


acebase.ref('shoppingLists').remove().then(() => console.log('Shopping lists removed from acebase'));


async function createExampleItem() {
    const item = {
        name: 'Example Item',
        quantityDesired: 5,
        quantityAcquired: 0
    };
    
    const ref = await acebase.ref('items').push(item);

    console.log('Exemplo de Item criado com ID:', ref.key);

    return {item:item, id: ref.key};
}

// Função para criar um exemplo de lista de compras que contém o item
async function createExampleShoppingList({item, id}) {
    const listaDeCompras = {
        items: [
            {
                id: id,
                name: item.name,
                quantityDesired: item.quantityDesired,
                quantityAcquired: item.quantityAcquired
            }
        ]
    };

    const ref = await acebase.ref('shoppingLists').push(listaDeCompras);
    console.log('Exemplo de Lista de Compras criado com ID:', ref.key);

}

// Inicia criando o exemplo de item e, em seguida, a lista de compras
createExampleItem()
    .then(item => createExampleShoppingList(item))
    .then(() => {
        // Inicia o servidor após criar os exemplos
        app.listen(port, () => {
            console.log(`Server is listening on port: ${port}`);
        });
    })
    .catch(error => console.error('Error creating example:', error));



const acebaseShoppingListRef =  acebase.ref('shoppingLists');
const mongooseShoppingListRef = schemas.ShoppingList;

async function syncShoppingLists() {
    const acebaseShoppingLists = await acebaseShoppingListRef.once('value');
    //console.log('Shopping lists from acebase:', acebaseShoppingLists.val());
    
    // ----------------- ACEBASE TO MONGO -----------------
    for (const acebaseShoppingListKey in acebaseShoppingLists.val()) {

        const acebaseShoppingList = acebaseShoppingLists.val()[acebaseShoppingListKey];

        const mongoShoppingList = await mongooseShoppingListRef.find({id: acebaseShoppingListKey}).exec();

        if (mongoShoppingList.length === 0) {
            
            console.log('Shopping list not found in mongo, creating it:', acebaseShoppingList)

            await createMongoShoppingList(acebaseShoppingList, acebaseShoppingListKey).catch(err => console.log(err));        
        }

        else {
            
            console.log('Shopping list from mongo:', mongoShoppingList)
            
            console.log('Shopping list from acebase:', acebaseShoppingList)
            
            var acebaseItems = acebaseShoppingList.items;
            var mongoItems = mongoShoppingList[0].items;

            console.log('Acebase items:', acebaseItems)
            console.log('Mongo items:', mongoItems)

            var isDifferent = areItemsListDifferent(acebaseItems, mongoItems);
            
            if (isDifferent) {
                
                // warning: the entryDate field is being updated on ALL items (idk if for Last Write Wins its a problem)
                mongoShoppingList[0].items = acebaseItems;
                await mongoShoppingList[0].save()
                .then(() => console.log('Shopping list updated in MongoDB', mongoShoppingList[0].id))
                .catch(err => console.log(err));
            }
            else {
                console.log('No need to update shopping list', mongoShoppingList[0].id)
            }
        }
    }

    // ----------------- MONGO TO ACEBASE -----------------
    const mongoShoppingLists = await mongooseShoppingListRef.find().exec();
    //console.log('Shopping lists from mongo:', mongoShoppingLists);

    for (const mongoShoppingListKey in mongoShoppingLists) {
            
        const mongoShoppingList = mongoShoppingLists[mongoShoppingListKey];

        const acebaseShoppingList = await acebaseShoppingListRef.child(mongoShoppingList.id).once('value');

        if (!acebaseShoppingList.exists()) {
    
            console.log('Shopping list not found in acebase, creating it:', mongoShoppingList)
            
            acebaseItems = [];
            for (const mongoShoppingListItemKey in mongoShoppingList.items) {
                    
                const mongoShoppingListItem = mongoShoppingList.items[mongoShoppingListItemKey];

                const item = {
                    id: mongoShoppingListItem.id,
                    name: mongoShoppingListItem.name,
                    quantityDesired: mongoShoppingListItem.quantityDesired,
                    quantityAcquired: mongoShoppingListItem.quantityAcquired
                }

                acebaseItems.push(item);
            }

            const newShoppingList = {
                id: mongoShoppingList.id,
                items: acebaseItems
            }

            await acebaseShoppingListRef.child(mongoShoppingList.id).set(newShoppingList)
            .then(() => console.log('New shopping list created in Acebase', newShoppingList.id))
            .catch(err => console.log(err));
        }
            
    }

}

async function createMongoShoppingList(acebaseShoppingList, acebaseShoppingListKey) {
    const mongoItems = [];
    for (const acebaseShoppingListItemKey in acebaseShoppingList.items) {
        
        const acebaseShoppingListItem = acebaseShoppingList.items[acebaseShoppingListItemKey];

        const item = {
            id: acebaseShoppingListItem.id,
            name: acebaseShoppingListItem.name,
            quantityDesired: acebaseShoppingListItem.quantityDesired,
            quantityAcquired: acebaseShoppingListItem.quantityAcquired
        }

        mongoItems.push(item);
    }

    //console.log("Mongo items:", mongoItems)
    const newShoppingList = new schemas.ShoppingList({
        id: acebaseShoppingListKey,
        items: mongoItems
    })
    

    await newShoppingList.save()
       .then(() => console.log('New shopping list created in MongoDB', newShoppingList.id))
        .catch(err => console.log(err));
}

function areItemsListDifferent(acebaseItems, mongoItems) {

    if (acebaseItems.length !== mongoItems.length) {
        return true;
    }

    for (const acebaseItemKey in acebaseItems) {

        const acebaseItem = acebaseItems[acebaseItemKey];
        const mongoItem = mongoItems.find(item => item.id === acebaseItem.id);
        if (mongoItem) {
            if (acebaseItem.name !== mongoItem.name) {
                return true;
            }
            if (acebaseItem.quantityDesired !== mongoItem.quantityDesired) {
                return true;
            }
            if (acebaseItem.quantityAcquired !== mongoItem.quantityAcquired) {
                return true;
            }
        }

        else {
            return true;
        }
    }
    return false;
}

setInterval(() => syncShoppingLists(), 5000);
