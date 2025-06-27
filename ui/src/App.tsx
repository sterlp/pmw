import Router, { Route, Switch } from "crossroad";
import { useEffect } from "react";
import { Col, Container, ListGroup, Nav, Navbar, Row } from "react-bootstrap";
import "./App.css";
import { useServerObject } from "./shared/http-request";
import WorkflowPage from "./workflow/workflow-page";
const BASE = "/pmw-ui";

const HomePage = () => (
    <main>
        <h2>Main Content</h2>
        <p>Welcome to the admin dashboard!</p>
    </main>
);

function App() {
    const workflows = useServerObject<string[]>("/pmw-api/workflows");
    useEffect(workflows.doGet, []);

    return (
        <Router>
            <Navbar bg="dark" variant="dark" expand="lg">
                <Container fluid>
                    <Navbar.Brand href="#">PMW Admin UI</Navbar.Brand>
                    <Navbar.Toggle aria-controls="top-navbar" />
                    <Navbar.Collapse id="top-navbar">
                        <Nav className="ms-auto">
                            <Nav.Link href="#profile">Profile</Nav.Link>
                            <Nav.Link href="#logout">Logout</Nav.Link>
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
        </Router>
    );
}

export default App;
