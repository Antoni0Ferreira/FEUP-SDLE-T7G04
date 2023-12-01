
class GCounter {
    constructor(id) {
        this.id = id;
        this.map = new Map();
    }

    increment(sumValue = 1) {
        let result = new GCounter(this.id);
        this.map[this.id] = (this.map[this.id] || 0) + sumValue;
        result.map[this.id] = this.map[this.id];
        return result;
    }

    equals(other) {
        return this.map[this.id] === other.map[this.id];
    }

    localValue() {
        let result = 0;
        result += this.map[this.id] || 0;
        return result;
    }

    readValues() {
        let result = 0;
        for (let key in this.map) {
            result += this.map[key];
        }
        return result;
    }

    merge(other) {
        for (let key in other.map) {
            if(this.map[key] === undefined || this.map[key] < other.map[key]) {
                this.map[key] = other.map[key];
            }
        }
    }

    toString() {
        let result = "GCounter: ( ";
        for (let key in this.map) {
            result += key + ": " + this.map[key] + ", ";
        }
        result += ")";
        return result;
    }
}

const counter1 = new GCounter("A");
const counter2 = new GCounter("B");

counter1.increment(1);
counter2.increment(3);

console.log(counter1.toString());
console.log(counter2.toString());

counter1.merge(counter2);
console.log(counter1.toString());
console.log(counter2.toString());

counter1.increment(2);
counter2.increment(1);

console.log("After incrementing");
console.log(counter1.toString());
console.log(counter2.toString());

counter1.merge(counter2);

console.log("After merging");
console.log(counter1.toString());
console.log(counter2.toString());

// ----------------------------------------------------------

class GMap {
    constructor() {
        this.map = new Map();
    }

    toString() {
        let result = "GMap:\n";
        for (let [key, value] of this.map) {
            result += key + ": " + value + "\n";
        }
        return result;
    }

    set (key, value) {
        if (!this.map.has(key)) {
            this.map.set(key, value);
        } else {
            this.map.set(key, value)
        }
    }

    joinValues(value1, value2) {
        return value1 + value2;
    }

    join (other) {
        const entries = [...this.map.entries()];
        const otherEntries = [...other.map.entries()];

        let i = 0;
        let j = 0;

        while (i < entries.length || j < otherEntries.length) {
            if (i === entries.length && (j >= otherEntries.length || entries[i][0] < otherEntries[j][0])) {
                i++;
            } else if (j < otherEntries.length && (i >= entries.length || entries[i][0] > otherEntries[j][0])) {
                this.set(entries[i][0], entries[i][1]);
                j++;
            } else if (i < entries.length && j < otherEntries.length) {
                this.set(entries[i][0], this.joinValues(this.map.get(entries[j][0]), other.map.get(otherEntries[j][1])));
                i++;
                j++;
            }
                
        }
    }
}

// Example usage:
const map1 = new GMap();
const map2 = new GMap();

map1.set("key1", 1);
map1.set("key2", 2);

map2.set("key2", 3);
map2.set("key3", 4);

console.log("Map 1:");
console.log(map1.toString());

console.log("Map 2:");
console.log(map2.toString());

map1.join(map2);

console.log("After Join:");
console.log(map1.toString());

// ----------------------------------------------------------

class RwCounter {
    constructor(id, jointc) {
      this.b = new Map();
      this.id = id;
  
      if (jointc) {
        this.b = new Map(jointc);
      }
    }
  
    operatorEquals(o) {
      if (this === o) {
        return this;
      }
      if (this.b !== o.b) {
        this.b = new Map(o.b);
      }
      this.id = o.id;
      return this;
    }
  
    context() {
      return this.b;
    }
  
    inc(val = 1) {
      const r = new RwCounter();
      const firstVal = this.b.get(this.id) ? this.b.get(this.id).first : 0;
      const secondVal = this.b.get(this.id) ? this.b.get(this.id).second : 0;
  
      const newData = {
        first: firstVal + val,
        second: secondVal,
      };
  
      this.b.set(this.id, newData);
      r.b.set(this.id, newData);
      return r;
    }
  
    dec(val = 1) {
      const r = new RwCounter();
      const firstVal = this.b.get(this.id) ? this.b.get(this.id).first : 0;
      const secondVal = this.b.get(this.id) ? this.b.get(this.id).second : 0;
  
      const newData = {
        first: firstVal,
        second: secondVal + val,
      };
  
      this.b.set(this.id, newData);
      r.b.set(this.id, newData);
      return r;
    }
  
    reset() {
      const r = new RwCounter();
      r.b.clear();
      return r;
    }
  
    fresh() {
      this.b.clear();
    }
  
    read() {
      let ac = { first: 0, second: 0 };
      for (const [key, value] of this.b.entries()) {
        ac.first += value.first;
        ac.second += value.second;
      }
      return ac.first - ac.second;
    }
  
    join(o) {
      for (const [key, value] of o.b.entries()) {
        if (!this.b.has(key)) {
          this.b.set(key, { first: 0, second: 0 });
        }
        this.b.get(key).first += value.first;
        this.b.get(key).second += value.second;
      }
    }
  
    toString() {
      return `ResetWinsCounter: ${Array.from(this.b.entries())}`;
    }
  }
  

  // Example usage:
const rwc1 = new RwCounter("i");
const rwc2 = new RwCounter("j");

rwc1.inc();
rwc1.inc(2);
rwc1.dec();
rwc2.inc(5);

console.log(rwc1.toString());
console.log(rwc2.toString());

rwc1.join(rwc2);

console.log(rwc1.toString());
console.log(rwc1.read());
console.log("Reset:", rwc2.reset().toString());
console.log("Delta:", rwc2.inc(1).toString());

rwc1.join(rwc2);

console.log(rwc1.toString());
console.log(rwc1.read());

rwc2.join(rwc1);
rwc2.reset();
rwc1.fresh();

console.log(rwc1.toString());

rwc1.inc();

console.log(rwc1.toString());

rwc1.join(rwc2);

console.log(rwc1.toString());
console.log(rwc1.read());