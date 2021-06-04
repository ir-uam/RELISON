%
% Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
% de Madrid, http://ir.ii.uam.es.
%
%  This Source Code Form is subject to the terms of the Mozilla Public
%  License, v. 2.0. If a copy of the MPL was not distributed with this
%  file, You can obtain one at http://mozilla.org/MPL/2.0/.
%

function mf = matrixforest(adjMatrix, numUsers, alpha)
% MATRIXFOREST Implementation of the matrix forest algorithm.
%   @args adjMatrix adjacency matrix
%   @args numUsers number of users in the network
%   @args alpha free parameter controlling the importance of the Laplacian matrix
%   @args k maximum distance from the target user
%
%   @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
%   @author Pablo Castells (pablo.castells@uam.es)
%
    D = diag(sum(adjMatrix));
    L = D - adjMatrix;
    X = eye(numUsers) + alpha.*L;
    
    mf = inv(X);
end