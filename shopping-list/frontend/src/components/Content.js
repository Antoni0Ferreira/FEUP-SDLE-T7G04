import React, { useState, useEffect } from "react";
import Toggle from "react-toggle";
import "react-toggle/style.css";
import { CommunicationComponent, OpORSet, OpCounter, OpLwwRegister } from "./crdt";

const Content = () => {
  
  const [localLwwRegister,setLocalLwwRegister] = useState(new OpLwwRegister("lwwDemo", false));
  const [localOpCounter,setLocalOpCounter] = useState(new OpCounter("counterDemo"));
  const [localOpORSet,setLocalOpORSetCounter] = useState(new OpORSet("orSetDemo"));
  const [orInput, setOrInput] = useState("");
  const [communicationComponent,setCommunicationComponent] = useState(new CommunicationComponent({
    localLwwRegister: localLwwRegister,
    localOpCounter: localOpCounter,
    localOpORSet: localOpORSet,
  }));

  useEffect(() => {
    communicationComponent.addCRDT(localLwwRegister);
    communicationComponent.addCRDT(localOpCounter);
    communicationComponent.addCRDT(localOpORSet);
    communicationComponent.setupApiRoutes("/api", "/api/initial", "/api/lp");
    communicationComponent.start();
    console.log("Communication Component: " + JSON.stringify(communicationComponent));
  }, [communicationComponent, localLwwRegister, localOpCounter, localOpORSet]);

  const counterChanged = (increase) => {
    const operation = { increase };
    setLocalOpCounter(localOpCounter.downstream(operation));
    communicationComponent.sendToServer(localOpCounter, operation);
    setCommunicationComponent(new CommunicationComponent({
      localLwwRegister: localLwwRegister,
      localOpCounter: localOpCounter,
      localOpORSet: localOpORSet,
    }))

  };

  const toggleChanged = (isChecked) => {
    const operation = { value: isChecked, timestamp: new Date().getTime() };
    setLocalLwwRegister(localLwwRegister.downstream(operation));
    communicationComponent.sendToServer(localLwwRegister, operation);
    setCommunicationComponent(new CommunicationComponent({
      localLwwRegister: localLwwRegister,
      localOpCounter: localOpCounter,
      localOpORSet: localOpORSet,
    }))
  };

  const addElementToOrSet = () => {
    const input = orInput;
    if (input) {
      const operation = { element: { element: input, uniqueID: Math.floor(Math.random() * 1000000000) }, add: true };
      setLocalOpORSetCounter(localOpORSet.downstream(operation));
      communicationComponent.sendToServer(localOpORSet, operation);
      setCommunicationComponent(new CommunicationComponent({
        localLwwRegister: localLwwRegister,
        localOpCounter: localOpCounter,
        localOpORSet: localOpORSet,
      }))
      setOrInput("");
    } else {
      console.log("Please enter a value");
    }
  };

  const removeElementFromORSet = (elem) => {
    const idsToRemove = localOpORSet.getIDsToRemove(elem);
    const operation = { element: idsToRemove, add: false };
    setLocalOpORSetCounter(localOpORSet.downstream(operation));
    communicationComponent.sendToServer(localOpORSet, operation);
    setCommunicationComponent(new CommunicationComponent({
      localLwwRegister: localLwwRegister,
      localOpCounter: localOpCounter,
      localOpORSet: localOpORSet,
    }))

  };

  const handleInput = (event) => {
    setOrInput(event.target.value);
  };

  const elementsToPresent = localOpORSet.setToDisplay().map((element) => (
    <li className="shoppingElement" id={`item${element.element}`} key={element.uniqueID}>
      {element.element}
      <span>
        <button className="boughtItemButton" onClick={() => removeElementFromORSet(element)}>
          <span className="glyphicon glyphicon-check" />
        </button>
        <button className="decrementButton" onClick={() => removeElementFromORSet(element)}>
          <span className="glyphicon glyphicon-minus" />
        </button>
      </span>
    </li>
  ));

  return (
    <div className="Content">
      <div className="toggleContainer">
        <label htmlFor="myToggle" className="toggleLabel">
          Out of money?
        </label>
        <Toggle
          id="myToggle"
          icons={false}
          checked={localLwwRegister.value}
          onChange={(myToggle) => toggleChanged(myToggle.target.checked)}
        />
      </div>
      <br />
      <div className="counterContainer">
        <label className="labelForCounter" htmlFor="myCounter">
          Budget for this month:
        </label>
        <span id="myCounter">
          <label className="counterLabel">{localOpCounter.value}€</label>
          <button className="incrementButton" onClick={() => counterChanged(true)}>
            <span className="glyphicon glyphicon-plus" />
          </button>
          <button className="decrementButton" onClick={() => counterChanged(false)}>
            <span className="glyphicon glyphicon-minus" />
          </button>
        </span>
      </div>
      <br />
      <br />
      <div>
        <ul className="shoppingList">{elementsToPresent}</ul>
        <div className="addElementToSetContainer">
          <input
            id="addItemField"
            className="addShoppingItemField"
            type="text"
            value={orInput}
            onChange={handleInput}
            placeholder="Add item..."
          />
          <button id="addItemBtn" className="incrementButton addItemBtn" onClick={addElementToOrSet}>
            <span className="glyphicon glyphicon-plus" />
          </button>
        </div>
      </div>
    </div>
  );
};

export default Content;
