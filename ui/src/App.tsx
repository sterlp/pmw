import { Route, Switch, useUrl } from "crossroad";
import { lazy, useEffect } from "react";
import { Col, Container, ListGroup, Nav, Navbar, Row } from "react-bootstrap";
import "./App.css";
const WorkflowPage = lazy(() => import("./workflow/workflow-page"));
import { useServerObject } from "spt-ui-lib";
const BASE = "/pmw-ui";
import * as Icon from "react-bootstrap-icons";

const HomePage = () => (
    <main>
        <h2>Main Content</h2>
        <p>Welcome to the admin dashboard!</p>
    </main>
);

function App() {
    const workflows = useServerObject<string[]>("/pmw-api/workflows");
    const [url, _] = useUrl();
    useEffect(workflows.doGet, []);
    return (
        <>
            <Navbar bg="dark" variant="dark" expand="lg">
                <Container fluid>
                    <Navbar.Brand href="#">PMW Admin UI</Navbar.Brand>
                    <Navbar.Toggle aria-controls="top-navbar" />
                    <Navbar.Collapse id="top-navbar">
                        <Nav className="ms-auto">
                            <Nav.Link
                                active={false}
                                target="_blank"
                                href="/task-ui"
                            >
                                Task UI
                            </Nav.Link>
                            <Nav.Link
                                href="https://github.com/sterlp/pmw"
                                target="_blank"
                            >
                                <Icon.Github />
                            </Nav.Link>
                        </Nav>
                    </Navbar.Collapse>
                </Container>
            </Navbar>

            <Container fluid>
                <Row>
                    <Col sm={2} className="bg-light vh-100 p-0">
                        <ListGroup variant="flush">
                            {workflows.data?.map((workflow) => (
                                <ListGroup.Item
                                    action
                                    active={
                                        url.path.indexOf(`/${workflow}`) !== -1
                                    }
                                    key={workflow}
                                    href={`${BASE}/workflows/${workflow}`}
                                >
                                    {workflow}
                                </ListGroup.Item>
                            ))}
                        </ListGroup>
                    </Col>
                    <Col sm={10} className="p-4">
                        <Switch>
                            <Route path={BASE} component={HomePage} />
                            <Route
                                path={`${BASE}/workflows/:id`}
                                component={WorkflowPage}
                            />
                        </Switch>
                    </Col>
                </Row>
            </Container>
        </>
    );
}

export default App;
