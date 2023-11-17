import './App.css';
import ShoppingList from './components/ShoppingList';
import Home from './components/Home';
import {Routes,Route} from "react-router-dom"; 

function App() {
  return (
    <>
      <Routes>
        <Route path='/:id' element={<ShoppingList />} />
      </Routes>
    <div className="App">
    </div>
    </>
  );
}

export default App;
