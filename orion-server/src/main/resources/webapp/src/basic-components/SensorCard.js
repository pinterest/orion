import React from "react";
import { Box, Typography, Card, CardContent, Tooltip } from "@material-ui/core";
import CheckCircleIcon from "@material-ui/icons/CheckCircle";
import ErrorIcon from "@material-ui/icons/Error";

export function SensorCard(props) {
  return (
    <Card>
      <CardContent>
        <Box display="flex" alignItems="center" justifyContent="space-between">
          <Typography color={props.m.previousSuccess ? "textPrimary" : "error"}>
            {props.m.sensor.name}
          </Typography>
        </Box>
        <Box>
          <Tooltip
            title={
              props.m && props.m.previousError
                ? props.m.previousError.message
                : "Operating normally"
            }
            aria-label="add"
          >
            {props.m.previousSuccess ? (
              <CheckCircleIcon style={{ color: "green" }} />
            ) : (
              <ErrorIcon style={{ color: "red" }} />
            )}
          </Tooltip>
        </Box>
        <Typography>Every {props.m.sensor.interval} seconds</Typography>
        <Typography>
          Previous execution:{" "}
          {new Date(props.m.previousFinishTime).toLocaleString()}
        </Typography>
      </CardContent>
    </Card>
  );
}
