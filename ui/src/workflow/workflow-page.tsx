import { useEffect } from "react";
import { Tab, Tabs } from "react-bootstrap";
import {
    HttpErrorView,
    LoadingView,
    TriggerGroupListView,
    useServerObject,
} from "spt-ui-lib";
import type { WorkflowDiagram } from "../server-pwm-api";
import WorkflowUmlView from "./views/workflow-uml.view";

const WorkflowPage = ({ id }: { id: string }) => {
    const active = "active";
    const history = "history";
    const workflow = useServerObject<WorkflowDiagram>("/pmw-api/workflows/");
    useEffect(() => {
        workflow.doGet(id, { cache: true });
    }, [id]);

    return (
        <main>
            <h2>Workflow {id}</h2>
            <HttpErrorView error={workflow.error} />
            {workflow.isLoading ? <LoadingView /> : undefined}
            {workflow.data ? (
                <WorkflowUmlView workflow={workflow.data} />
            ) : undefined}

            <Tabs>
                <Tab
                    eventKey={active}
                    title="Active"
                    mountOnEnter
                    unmountOnExit
                >
                    <TriggerGroupTabContent
                        url="/spring-tasks-api/triggers-grouped"
                        tagId={id}
                        onPath="triggers"
                    />
                </Tab>
                <Tab
                    eventKey={history}
                    title="History"
                    mountOnEnter
                    unmountOnExit
                >
                    <TriggerGroupTabContent
                        url="/spring-tasks-api/history-grouped"
                        tagId={id}
                        onPath="history"
                    />
                </Tab>
            </Tabs>
        </main>
    );
};
export default WorkflowPage;

interface TriggerGroupTabContentProps {
    url: string;
    tagId: string;
    onPath: string;
}

const TriggerGroupTabContent: React.FC<TriggerGroupTabContentProps> = ({
    tagId,
    onPath,
}) => {
    return (
        <div className="p-3" key={`${tagId}-${onPath}`}>
            <TriggerGroupListView
                url={`/spring-tasks-api/${onPath}-grouped`}
                filter={{ tag: tagId }}
                onGroupClick={(t: string) => {
                    window.location.href = `/task-ui/${onPath}?search=${t}&tag=${tagId}`;
                }}
            />
        </div>
    );
};
