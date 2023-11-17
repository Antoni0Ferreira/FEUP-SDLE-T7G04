import React, { useState, useEffect } from 'react';
import {useParams } from 'react-router-dom';
import axios from 'axios';

const ShoppingList = () => {
  const [shoppingList, setShoppingList] = useState({
    id: '',
    items: []
  });

  const [item, setItem] = useState('');
  const [quantity, setQuantityDesired] = useState('');

  const { id } = useParams();


  useEffect(() => {
    axios.get(`/api/shoppingList/${id}`)
    .then(response => setShoppingList(response.data))
    .catch(error => console.log(error));
  }, [])

  const handleAddItem = (event) => {
    event.preventDefault();

    const newItem = {
      name: item,
      quantityDesired: quantity
    };

    // console log repsonse in then
    const response = axios.post(`/api/shoppingList/${id}/items`, {item:newItem})
    .then(response => {setShoppingList(response.data); console.log(response.data)})
    .catch(error => console.log(error));
    
    setItem('');
    setQuantityDesired('');
  }

  const handleDeleteItem = (itemId) => {
    axios.delete(`/api/shoppingList/${id}/items/${itemId}`)
    .then(response => setShoppingList(response.data))
    .catch(error => console.log(error));
  };

  return (
    <div>
      <h1>Shopping List</h1>
      <ul>
        {shoppingList.items.map((item) => (
          <li key={item.id}>{item.name} - Desired Quantity: {item.quantityDesired} - Acquired Quantity: {item.quantityAcquired}
          <button onClick={() => handleDeleteItem(item.id)}> Remove </button></li>
        ))}

      </ul>

      <form onSubmit={handleAddItem}>
        <label>Item:</label>
        <input type="text" value={item} onChange={(event) => setItem(event.target.value)} />
        <label>Quantity:</label>
        <input type="text" value={quantity} onChange={(event) => setQuantityDesired(event.target.value)} />
        <input type="submit" value="Add" />
      </form>
    </div>
  );
};


export default ShoppingList;
