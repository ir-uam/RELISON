%
% Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
% de Madrid, http://ir.ii.uam.es.
%
%  This Source Code Form is subject to the terms of the Mozilla Public
%  License, v. 2.0. If a copy of the MPL was not distributed with this
%  file, You can obtain one at http://mozilla.org/MPL/2.0/.
%


function lpi = lpiMatrix(adjMatrix, numUsers, b, k)
% LPIMATRIX Implementation of the local path index algorithm.
%   @args adjMatrix adjacency matrix
%   @args numUsers number of users in the network
%   @args b dampening factor
%   @args k maximum distance from the target user
%
%   @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
%   @author Pablo Castells (pablo.castells@uam.es)
%
    aux = eye(numusers);
    aux = adjMatrix * aux;
    beta = 1;
    lpi = zeros(numUsers,numUsers);
    for i = 2:k
        aux = aux * adjMatrix;
        lpi = lpi + beta.*aux;
        beta = beta * b;
    end
end