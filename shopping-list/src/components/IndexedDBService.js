import Dexie from 'dexie';

// Crie um banco de dados IndexedDB usando Dexie
const db = new Dexie('ShoppingListDatabase');
db.version(1).stores({ shoppingList: '++id, name, items' });

const indexedDBService = {
  // Adicione uma lista de compras ao banco de dados
  addShoppingList: async (name, items) => {
    const id = await db.shoppingList.add({ name, items });
    return id;
  },

  // Obtenha todas as listas de compras do banco de dados
  getAllShoppingLists: async () => {
    const lists = await db.shoppingList.toArray();
    return lists;
  },

  // Obtenha uma lista de compras por ID do banco de dados
  getShoppingListById: async (id) => {
    const list = await db.shoppingList.get(id);
    return list;
  },
};

export default indexedDBService;
