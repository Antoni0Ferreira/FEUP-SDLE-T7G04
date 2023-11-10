const mongoose = require('mongoose');
const Schema = mongoose.Schema;

const ItemSchema = new Schema({ 
    id : {type:Number},
    name : {type:String},
    quantityDesired : {type:Number},
    quantityAcquired : {type:Number},
    entryDate : {type:Date, default:Date.now}
});

const ShoppingListSchema = new Schema({
    id : {type:Number},
    items : [ItemSchema]
});

const Item = mongoose.model('Item', ItemSchema, 'Item');
const ShoppingList = mongoose.model('ShoppingList', ShoppingListSchema, 'ShoppingList');
const mySchemas = {'Item':Item, 'ShoppingList':ShoppingList};

module.exports = mySchemas;