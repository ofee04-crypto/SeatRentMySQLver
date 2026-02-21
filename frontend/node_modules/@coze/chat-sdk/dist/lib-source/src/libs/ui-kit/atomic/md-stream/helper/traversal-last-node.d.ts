import type { Parent, RootContent } from 'mdast';
import type { RootContentLocal } from '../ast';
export declare const traversalLastNode: (node: RootContentLocal, parent: RootContentLocal | undefined, handle: (content: RootContentLocal | undefined, parent?: RootContentLocal) => RootContentLocal[]) => RootContentLocal[];
export declare const getLastNode: (node: Parent) => RootContent | undefined;
export declare const isConsumeTextToShow: (node: Parent) => boolean;
