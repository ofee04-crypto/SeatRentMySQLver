import { FC, PropsWithChildren } from 'react';
import { type ViewProps } from '@tarojs/components';
export declare const CenterAlignedBox: FC<PropsWithChildren<{
    width?: number;
    height?: number;
    className?: string;
} & Omit<ViewProps, 'ref'>>>;
