import EdgeCurveProgram, { EdgeCurvedArrowProgram } from "./vendor/esm/sigma-edge-curve.js";

window.relisonSigmaEdgePrograms = {
    curve: EdgeCurveProgram,
    curvedArrow: EdgeCurvedArrowProgram,
};
window.dispatchEvent(new CustomEvent("relison-sigma-edge-curve-ready"));
