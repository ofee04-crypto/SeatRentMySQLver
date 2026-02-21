export const safeJSONParse = (v, defaultValue = null) => {
    if (typeof v === 'object') {
        return v;
    }
    try {
        return JSON.parse(String(v));
    }
    catch (e) {
        return defaultValue;
    }
};
//# sourceMappingURL=safe-json-parse.js.map