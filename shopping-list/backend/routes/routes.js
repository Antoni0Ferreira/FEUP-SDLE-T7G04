const express = require('express');
const router = express.Router();
const schemas = require('../models/schemas');

router.get('/', async (req, res) => {
    const newItem = new schemas.Item({
        id: 1,
        name: 'Arroz',
        quantityDesired: 1,
        quantityAcquired: 0,
    });

    const saveItem = await newItem.save();
    if (saveItem) {
        res.send(saveItem);
    }

    res.end()

});

module.exports = router;
