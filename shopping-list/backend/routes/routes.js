const express = require('express');
const router = express.Router();
const schemas = require('../models/schemas');

router.get('/', async (req, res) => {
    console.log("GET REQUEST");

    var remIP = req.ip;
    console.log("Client IP: " + remIP);

    
});

module.exports = router;
