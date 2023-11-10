import React, { useState, useEffect } from 'react';
import {useParams } from 'react-router-dom';
import indexedDBService from '../services/IndexedDBService';

const ShoppingList = () => {
  const { id } = useParams();
  const [shoppingList, setShoppingList] = useState(null);
  const [newItemName, setNewItemName] = useState('');


  useEffect(() => {
    const fetchData = async () => {
      try {
        const db = await indexedDBService.initializeDB();
        const transaction = db.transaction(['shoppingLists'], 'readonly');
        const shoppingListStore = transaction.objectStore('shoppingLists');
        const request = await shoppingListStore.get(parseInt(id, 10));

        request.onsuccess = (event) => {
          const list = event.target.result;
          setShoppingList(list);
        };

        request.onerror = (event) => {
          console.error(event.target.error);
        };
      } catch (error) {
        console.error('Error during data fetch:', error);
      }
    };

    fetchData();
  }, [id]);

  const handleAddItem = async () => {
    if (!newItemName) return;

    const db = await indexedDBService.initializeDB();
    const transaction = db.transaction(['shoppingLists'], 'readwrite');
    const shoppingListStore = transaction.objectStore('shoppingLists');

    const request = shoppingListStore.get(parseInt(id, 10));

    request.onsuccess = () => {
      const list = request.result;
      const newItem = {
        name: newItemName,
        quantityDesired: 1,
        quantityAcquired: 0,
      };

      list.items.push(newItem);

      const updateRequest = shoppingListStore.put(list);

      updateRequest.onsuccess = () => {
        setNewItemName(''); // Limpar o campo após adicionar o item
        setShoppingList(list);
      };
    };
  };

  const handleRemoveItem = async (index) => {
    const updatedList = { ...shoppingList };
    updatedList.items.splice(index, 1);

    updateShoppingList(updatedList);
  }

  const handleIncreaseQuantityDesired = async (index) => {
    const updatedList = { ...shoppingList };
    updatedList.items[index].quantityDesired += 1;

    updateShoppingList(updatedList);
  };

  const handleDecreaseQuantityDesired = async (index) => {
    const updatedList = { ...shoppingList };
    updatedList.items[index].quantityDesired -= 1;

    updateShoppingList(updatedList);
  }

  const handleIncreaseQuantityAcquired = async (index) => {
    const updatedList = { ...shoppingList };
    updatedList.items[index].quantityAcquired += 1;

    updateShoppingList(updatedList);
  }

  const handleDecreaseQuantityAcquired = async (index) => {
    const updatedList = { ...shoppingList };
    updatedList.items[index].quantityAcquired -= 1;

    updateShoppingList(updatedList);
  }

  const updateShoppingList = async (updatedList) => {
    try {
      const db = await indexedDBService.initializeDB();
      const transaction = db.transaction(['shoppingLists'], 'readwrite');
      const shoppingListStore = transaction.objectStore('shoppingLists');
      const updateRequest = shoppingListStore.put(updatedList);

      updateRequest.onsuccess = () => {
        setShoppingList(updatedList);
      };

      updateRequest.onerror = (event) => {
        console.error(event.target.error);
      };
    } catch (error) {
      console.error('Error during data update:', error);
    }
  };

  return (
    <div>
      {shoppingList ? (
        <>
          <h1>{shoppingList.name}</h1>
          <div>
            <input
              type="text"
              placeholder="New Item Name"
              value={newItemName}
              onChange={(e) => setNewItemName(e.target.value)}
            />
            <button onClick={handleAddItem}>Add Item</button>
          </div>
          <ul>
            {shoppingList.items.map((item, index) => (
              <li key={index}>
                
                {item.name + " "}       
                Desired:  
                <button onClick={() => handleDecreaseQuantityDesired(index)}>
                  -
                </button>
                {item.quantityDesired} 
                <button onClick={() => handleIncreaseQuantityDesired(index)}>
                  +
                </button>
                
                Acquired: 
                <button onClick={() => handleDecreaseQuantityAcquired(index)}>
                  -
                </button>
                {item.quantityAcquired}
                <button onClick={() => handleIncreaseQuantityAcquired(index)}>
                  +
                </button>
                <button onClick={() => handleRemoveItem(index)}>
                  Remove Item
                </button>
              </li>
            ))}
          </ul>
        </>
      ) : (
        <p>Loading...</p>
      )}
    </div>
  );
};


export default ShoppingList;
