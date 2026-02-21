/// <reference types="react" />
interface ErrorRetryProps {
    errorText: string | React.ReactNode;
}
export declare const ErrorRetry: ({ errorText }: ErrorRetryProps) => import("react/jsx-runtime").JSX.Element;
export declare const ErrorRetryBtn: ({ onClick, retryText, }: {
    retryText: string;
    onClick: () => void;
}) => import("react/jsx-runtime").JSX.Element;
export {};
