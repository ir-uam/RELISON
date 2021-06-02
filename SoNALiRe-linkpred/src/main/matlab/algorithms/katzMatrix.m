%
% Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
% de Madrid, http://ir.ii.uam.es.
%
%  This Source Code Form is subject to the terms of the Mozilla Public
%  License, v. 2.0. If a copy of the MPL was not distributed with this
%  file, You can obtain one at http://mozilla.org/MPL/2.0/.
%


function katz = katzMatrix(adjMatrix, numUsers, b)
% KATZ Implementation of the Katz similarity algorithm.
%   @args adjMatrix adjacency matrix of the network.
%   @args numUsers number of users in the network.
%   @args b dampening factor.
%
%   @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
%   @author Pablo Castells (pablo.castells@uam.es)
%
    aux = eye(numUsers) - b.*adjMatrix;
    
    katz = inv(aux);
end