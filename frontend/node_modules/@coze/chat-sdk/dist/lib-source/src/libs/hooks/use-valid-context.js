import { useContext } from 'react';
import { isValidContext } from "../utils";
export const useValidContext = (context, errorMsg = 'Invalid chat frame context') => {
    const value = useContext(context);
    if (!isValidContext(value)) {
        throw new Error(errorMsg);
    }
    return value;
};
//# sourceMappingURL=use-valid-context.js.map