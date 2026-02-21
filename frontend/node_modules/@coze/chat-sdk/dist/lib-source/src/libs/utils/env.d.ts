export declare enum Region {
    OVERSEA = "oversea",
    CN = "cn"
}
export declare const getRegionApi: (region?: Region) => "https://api.coze.com" | "https://api.coze.cn";
