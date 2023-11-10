import React, { useState, useEffect } from 'react';
import indexedDBService from './IndexedDBService';

const ShoppingList = () => {
  const [shoppingLists, setShoppingLists] = useState([]);

  const addShoppingList = async () => {
    const newShoppingList = await indexedDBService.addShoppingList('Tomas List', 10);
    console.log("New Shopping List: ", newShoppingList);

    const updatedLists = await indexedDBService.getAllShoppingLists();
    setShoppingLists(updatedLists);
  };

  const getShoppingList = async () => {
    const shoppingList = await indexedDBService.getShoppingListById(1);
    console.log("Shopping List: ", shoppingList);
  };

  useEffect(() => {
    const fetchData = async () => {
      const lists = await indexedDBService.getAllShoppingLists();
      setShoppingLists(lists);
    };

    fetchData();
  }, []);

  return (
    <div>
      <h1>Shopping List</h1>

      <button onClick={addShoppingList}>Add Shopping List</button>

      <ul>
        {shoppingLists.map((shoppingList) => (
          <li key={shoppingList.id}>{shoppingList.name}</li>
        ))}
      </ul>

      <button onClick={getShoppingList}>Get Shopping List</button>
    </div>
  );
};

export default ShoppingList;
