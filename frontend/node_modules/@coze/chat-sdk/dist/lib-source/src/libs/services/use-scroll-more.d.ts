export declare const useScrollMore: () => {
    prevError: import("..").IMiniChatError | undefined;
    isNeedPrevLoadMore: boolean | undefined;
    upperThreshold: number;
    onScrollToUpper: (() => Promise<void>) | undefined;
};
