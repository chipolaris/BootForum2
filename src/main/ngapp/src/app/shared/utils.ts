
// utility method to check if an object has a cycle
export function hasCycle(obj: any): boolean {
  try {
    JSON.stringify(obj);
    return false;
  } catch {
    return true;
  }
}

// another version, slightly more performant and accurate but
// more resource intensive than JSON.stringify version above
export function hasCycle2(obj: any): boolean {
  const seen = new WeakSet();
  function detect(o: any): boolean {
    if (o && typeof o === 'object') {
      if (seen.has(o)) return true;
      seen.add(o);
      return Object.values(o).some(value => detect(value));
    }
    return false;
  }
  return detect(obj);
}

// deep clone an object
// sample use:
//   const safeGroup = deepClone(groupData);
export function deepClone(obj: any): any {
  return JSON.parse(JSON.stringify(obj));
}

// deep safe clone
export function deepCloneSafe<T>(input: T, options?: {
  handleCycle?: 'skip' | 'replace' | 'throw',
  cyclePlaceholder?: any
}): T {
  const seen = new WeakMap<object, any>();
  const {
    handleCycle = 'replace',
    cyclePlaceholder = '[Circular]'
  } = options || {};

  function clone(value: any): any {
    if (value === null || typeof value !== 'object') {
      return value; // primitives
    }

    if (seen.has(value)) {
      if (handleCycle === 'throw') {
        throw new Error('Circular reference detected');
      } else if (handleCycle === 'replace') {
        return cyclePlaceholder;
      } else {
        return undefined; // skip
      }
    }

    // Track this object
    const cloned: any = Array.isArray(value) ? [] : {};
    seen.set(value, cloned);

    for (const key of Object.keys(value)) {
      const val = value[key];
      const copied = clone(val);
      if (!(handleCycle === 'skip' && copied === undefined)) {
        cloned[key] = copied;
      }
    }

    return cloned;
  }

  return clone(input);
}

/*

Deep clone safe with paths


Sample Usage:

import { deepCloneSafeWithPaths } from './utils/deep-utils';

const obj: any = { name: 'Tree' };
obj.self = obj;

const { clone, cycles } = deepCloneSafeWithPaths(obj);

console.log('Cloned:', clone);
// Cloned: { name: 'Tree', self: '[Circular]' }

console.log('Cycles Detected:', cycles);
// Cycles Detected: [{ path: '$.self', value: [Circular] }]



*/
export interface CycleInfo {
  path: string;
  value: any;
}

export function deepCloneSafeWithPaths<T>(
  input: T,
  options?: {
    handleCycle?: 'skip' | 'replace' | 'throw',
    cyclePlaceholder?: any
  }
): { clone: T; cycles: CycleInfo[] } {
  const seen = new WeakMap<object, any>();
  const cycles: CycleInfo[] = [];
  const {
    handleCycle = 'replace',
    cyclePlaceholder = '[Circular]'
  } = options || {};

  function clone(value: any, path: string): any {
    if (value === null || typeof value !== 'object') {
      return value; // primitives
    }

    if (seen.has(value)) {
      // Cycle detected
      cycles.push({ path, value });

      if (handleCycle === 'throw') {
        throw new Error(`Circular reference detected at path: ${path}`);
      } else if (handleCycle === 'replace') {
        return cyclePlaceholder;
      } else {
        return undefined; // skip
      }
    }

    const cloned: any = Array.isArray(value) ? [] : {};
    seen.set(value, cloned);

    for (const key of Object.keys(value)) {
      const val = value[key];
      const subPath = Array.isArray(value)
        ? `${path}[${key}]`
        : `${path}.${key}`;
      const copied = clone(val, subPath);
      if (!(handleCycle === 'skip' && copied === undefined)) {
        cloned[key] = copied;
      }
    }

    return cloned;
  }

  const cloneResult = clone(input, '$');
  return { clone: cloneResult, cycles };
}

