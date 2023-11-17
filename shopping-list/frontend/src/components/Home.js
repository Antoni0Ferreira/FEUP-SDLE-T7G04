/*
import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useNavigate } from 'react-router-dom';

const Home = () => {
  const [shoppingLists, setShoppingLists] = useState([]);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchData = async () => {
      const db = await indexedDBService.initializeDB();
      const transaction = db.transaction(['shoppingLists'], 'readonly');
      const shoppingListStore = transaction.objectStore('shoppingLists');

      transaction.oncomplete = () => {
        console.log('Transaction completed');
      }

        transaction.onerror = () => {
            console.log('Transaction not opened due to error');
        }
      const request = await shoppingListStore.getAll();

      request.onsuccess = (event) => {
        console.log("Data fetch success")
        const lists = event.target.result;
        console.log("Lists:" + lists)
        setShoppingLists(lists);
      };

        request.onerror = (event) => {
            console.log("Data fetch error")
            console.log(event.target.error);
        };
        
    };

    fetchData();
  }, []);

  const handleCreateList = async () => {
    const db = await indexedDBService.initializeDB();
    const transaction = db.transaction(['shoppingLists'], 'readwrite');
    const shoppingListStore = transaction.objectStore('shoppingLists');

    const newList = {
      items: [],
    };

    const request = shoppingListStore.add(newList);

    request.onsuccess = () => {
      navigate(`/${request.result}`);
    };
  };

  return (
    <div>
      <h1>Shopping Lists</h1>
      <button onClick={handleCreateList}>Create New List</button>
      <ul>
        {Array.isArray(shoppingLists) ? shoppingLists.map((list) => (
          <li key={list.id}>
            <Link to={`/${list.id}`}>{list.id}</Link>
          </li>
        )) : null}
      </ul>
    </div>
  );
};

export default Home;*/