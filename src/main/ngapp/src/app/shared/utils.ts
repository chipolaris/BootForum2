
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
