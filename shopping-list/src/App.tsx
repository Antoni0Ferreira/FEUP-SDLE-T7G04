import { useState } from 'react'
import reactLogo from './assets/react.svg'
import viteLogo from '/vite.svg'
import {AutomergeUrl} from '@automerge/automerge-repo'
import {useDocument} from '@automerge/automerge-repo-react-hooks'
import './App.css'
import * as A from "@automerge/automerge"

import React from 'react';

interface CounterDoc {
  counter: A.Counter;
  asparagusCounter: A.Counter
}

function App({ docUrl }: { docUrl: AutomergeUrl }) {
  const [doc, changeDoc] = useDocument<CounterDoc>(docUrl);

  return (
    <>
      <nav className="navbar navbar-light bg-light p-3 shadow-sm">
        <div className="container-fluid">
          <span className="navbar-brand mb-0 h1 fs-2">Shopping List</span>
        </div>
      </nav>
      <div className="container">
        <div className="row gy-5 mt-3">
          <div className="col-4">
            <div className="d-flex justify-content-center">
              {/* Modified button with your preferred appearance */}
              <button
                type="button"
                className="btn mb-2 mb-md-0 btn-primary btn-block btn-lg p-3 shadow-sm"
              >
                Asparagus
              </button>
              <div className="d-flex flex-column justify-content-between ms-3">
                <button
                  type="button"
                  onClick={() => changeDoc((d) => d.asparagusCounter.increment(1))}
                  className="shadow-sm btn btn-sm btn-light btn-quantity"
                >
                  +
                </button>
                <button
                  type="button"
                  onClick={() => changeDoc((d) => d.asparagusCounter.increment(-1))}
                  className="shadow-sm btn btn-sm btn-light btn-quantity"
                >
                  -
                </button>
              </div>
            </div>
            <p className="text-muted">quantity: {doc && doc.asparagusCounter.value}</p>
          </div>
        </div>
      </div>
      <footer className="shadow-sm d-flex justify-content-between footer p-3 bg-light fixed-bottom">
        <p className="text-sm-start">SDLE 2023/2024</p>
        <p className="text-sm-end">T07G04</p>
      </footer>
    </>
  );
}

export default App;
