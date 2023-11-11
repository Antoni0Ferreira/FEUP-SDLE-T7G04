const express = require('express');
const cors = require('cors'); // access our server from different domains
const bodyParser = require('body-parser'); // mainly used for post form
const router = require('./routes/routes');
const mongoose = require('mongoose');
const ip = require('ip');
const crdt = require('react-crdt');
require('dotenv/config');

const app = express();

app.use(bodyParser.json());
app.use(bodyParser.urlencoded({extended: false}));

const corsOptions = {
    origin: "*",
    credentials:true,
    optionSuccessStatus:200
}

app.use(cors(corsOptions));

app.use('/', router);

const dbOptions = {useNewUrlParser: true, useUnifiedTopology: true};
mongoose.connect(process.env.DB_URI, dbOptions)
.then(() => console.log('Connected to DB'))
.catch(err => console.log(err));


const port = process.env.PORT || 4000;

const server = app.listen(port, () => {
    console.log(`Server running on port ${port}`);
    console.log(ip.address() + ':' + server.address().port);
} );



