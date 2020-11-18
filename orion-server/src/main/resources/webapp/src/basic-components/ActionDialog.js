import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Paper,
  Snackbar,
} from "@material-ui/core";
import MuiAlert from "@material-ui/lab/Alert";
import { Select, TextField } from "mui-rff";
import React, { useState } from "react";
import Draggable from "react-draggable";
import { Field, Form } from "react-final-form";

function PaperComponent(props) {
  return (
    <Draggable
      handle="#draggable-dialog-title"
      cancel={'[class*="MuiDialogContent-root"]'}
    >
      <Paper {...props} />
    </Draggable>
  );
}

function Alert(props) {
  return <MuiAlert elevation={6} variant="filled" {...props} />;
}

const fieldComponentMap = {
  select: Select,
  textField: TextField,
};

export default function ActionDialog({ clusterId, action, clearAction }) {
  const [actionToast, setActionToast] = useState(null);
  const dialogEnabled = action != null;

  const handleCloseDialog = () => {
    clearAction();
  };
  const handleCloseToast = (event, reason) => {
    if (reason === "clickaway") {
      return;
    }
    setActionToast(null);
  };

  const handleDialogForm = (values) => {
    if (!action) {
      return;
    }
    const { actionKey } = action;
    const url = "/api/clusters/" + clusterId + "/actions/" + actionKey;
    const payload = {
      attributes: {
        ...values,
      },
    };

    fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    })
      .then((response) => {
        if (!response.ok) {
          response.json().then((error) => {
            setActionToast({
              message: "Action failed: " + error.message,
              status: false,
            });
          });
          return null;
        } else {
          return response.text();
        }
      })
      .then((data) => {
        if (data) {
          setActionToast({
            message: "Action " + data + " launched",
            status: true,
          });
        }
      });
  };

  const hasForm = action && action.attributes ? true : false;
  const onSubmit = (values) => {
    const input = hasForm ? values : {};
    handleDialogForm(input);
    handleCloseDialog();
    return;
  };

  // special type of field just for node ids
  const NodeIdField = (p) => (
    <Field name="nodeIds" initialValue={action.nodeIds}>
      {() => null}
    </Field>
  );

  const generateActionForm = (attributes) => {
    return attributes.map((attr) => {
      if (attr.name === "nodeIds") {
        return <NodeIdField key={attr.name} />;
      }
      const Tag = fieldComponentMap[attr.type];
      return <Tag key={attr.name} {...attr} />;
    });
  };

  return (
    <React.Fragment>
      <Dialog
        open={dialogEnabled}
        onClose={handleCloseDialog}
        PaperComponent={PaperComponent}
        aria-labelledby="draggable-dialog-title"
        TransitionProps={{ exit: false }}
      >
        <DialogTitle style={{ cursor: "move" }} id="draggable-dialog-title">
          Confirm Node {action ? action.displayName : ""} Action
        </DialogTitle>
        <Form
          onSubmit={onSubmit}
          render={({ handleSubmit, form, pristine, submitting }) => (
            <React.Fragment>
              <DialogContent>
                <DialogContentText>
                  Are you sure you want to trigger{" "}
                  {action ? action.displayName : ""} on the selected nodes?
                </DialogContentText>
                {hasForm && generateActionForm(action.attributes)}
              </DialogContent>
              <DialogActions>
                {hasForm && (
                  <Button
                    onClick={form.reset}
                    disabled={submitting || pristine}
                  >
                    Reset
                  </Button>
                )}
                <Button
                  autoFocus
                  onClick={handleCloseDialog}
                  color="primary"
                  disabled={submitting}
                >
                  No
                </Button>
                <Button
                  onClick={handleSubmit}
                  color="primary"
                  disabled={submitting}
                >
                  Yes
                </Button>
              </DialogActions>
            </React.Fragment>
          )}
        />
      </Dialog>
      <Snackbar
        anchorOrigin={{
          vertical: "bottom",
          horizontal: "left",
        }}
        open={actionToast}
        autoHideDuration={2000}
        onClose={handleCloseToast}
      >
        {actionToast && (
          <Alert severity={actionToast.status ? "success" : "error"}>
            {actionToast.message}
          </Alert>
        )}
      </Snackbar>
    </React.Fragment>
  );
}
