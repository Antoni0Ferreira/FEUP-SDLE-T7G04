const mongoose = require('mongoose');
const Schema = mongoose.Schema;


const ShoppingListSchema = new Schema({
    id: { type: String },
    items: [
        {   
            id : { type: String },
            name: { type: String },
            quantityDesired: { type: Number },
            quantityAcquired: { type: Number },
            entryDate: { type: Date, default: Date.now }
        }
    ]
});

const ShoppingList = mongoose.model('ShoppingList', ShoppingListSchema, 'ShoppingList');
const mySchemas = {'ShoppingList':ShoppingList};

module.exports = mySchemas;