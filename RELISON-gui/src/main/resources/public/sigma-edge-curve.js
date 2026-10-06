import EdgeCurveProgram, { EdgeCurvedArrowProgram } from "https://esm.sh/@sigma/edge-curve@3.1.0?bundle";

window.relisonSigmaEdgePrograms = {
    curve: EdgeCurveProgram,
    curvedArrow: EdgeCurvedArrowProgram,
};
window.dispatchEvent(new CustomEvent("relison-sigma-edge-curve-ready"));
