// src/types/toast-ui-editor-viewer.d.ts
// or src/toast-ui-editor-viewer.d.ts

declare module '@toast-ui/editor/dist/toastui-editor-viewer' {
  // Using 'any' will silence the TS7016 error.
  // If you discover more specific types for the Viewer's constructor or methods,
  // you can replace 'any' with a more detailed interface.
  const Viewer: any;
  export default Viewer;
}

