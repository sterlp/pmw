import type { WorkflowDiagram } from "../../server-pwm-api";

const WorkflowUmlView = ({ workflow }: { workflow: WorkflowDiagram }) => {
    return (
        <div className="d-flex flex-column justify-content-center align-items-center">
            <p>
                <img
                    src={`data:image/svg+xml;base64,${workflow.svgBase64}`}
                    alt={workflow.id}
                />
            </p>
        </div>
    );
};
export default WorkflowUmlView;
