const dbName = 'ShoppingListDatabase';
const version = 11;

const initializeDB = () => {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(dbName, version);

    request.onupgradeneeded = (event) => {
      const db = event.target.result;

      if (!db.objectStoreNames.contains('shoppingLists')) {
        const shoppingListStore = db.createObjectStore('shoppingLists', { keyPath: 'id', autoIncrement: true });
        shoppingListStore.createIndex('items', 'items', { unique: false });
      }
    };

    request.onsuccess = (event) => {
      const db = event.target.result;
      resolve(db);
    };

    request.onerror = (event) => {
      reject(event.target.error);
    };
  });
};

const indexedDBService = {
  initializeDB,
  // Add more methods for interacting with IndexedDB as needed
};

export default indexedDBService;
