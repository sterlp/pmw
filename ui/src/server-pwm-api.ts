/* tslint:disable */
/* eslint-disable */

export interface WorkflowDiagram {
    id: string;
    plantUml: string;
    svgBase64: string;
}

export interface WorkflowInfo {
    id: string;
    name: string;
    steps: number;
}
