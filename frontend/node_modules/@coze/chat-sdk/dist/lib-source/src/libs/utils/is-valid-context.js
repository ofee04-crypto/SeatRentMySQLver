export const isValidContext = (context) => Object.keys(context)
    .map(keyName => context[keyName])
    .reduce((prevResult, currentProperty) => prevResult && currentProperty !== null, true);
//# sourceMappingURL=is-valid-context.js.map