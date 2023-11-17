import './App.css';
import ShoppingList from './components/ShoppingList';
import Home from './components/Home';
import Content from './components/Content';
import {Routes,Route} from "react-router-dom"; 

function App() {
  return (
    <>
      <Routes>
        <Route path='/:id' element={<ShoppingList />} />
        <Route path='/' element={<Content />} />
      </Routes>
    <div className="App">
    </div>
    </>
  );
}

export default App;
