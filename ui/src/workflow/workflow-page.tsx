import { useEffect } from "react";
import { useServerObject } from "../shared/http-request";
import { Spinner } from "react-bootstrap";

const WorkflowPage = ({ id }: { id: string }) => {
    const workflow = useServerObject<any>("/pmw-api/workflows/");

    useEffect(() => workflow.doGet(id, { cache: true }), [id]);

    return (
        <main>
            <h2>Workflow {id}</h2>
            <div className="d-flex flex-column justify-content-center align-items-center">
                {!workflow.isLoading &&
                workflow.data &&
                workflow.data.svgBase64 ? (
                    <p>
                        <img
                            src={`data:image/svg+xml;base64,${workflow.data.svgBase64}`}
                            alt="My SVG"
                        />
                    </p>
                ) : (
                    <>
                        <Spinner animation="border" />
                        <div className="mt-2">Loading...</div>
                    </>
                )}
            </div>
        </main>
    );
};
export default WorkflowPage;
