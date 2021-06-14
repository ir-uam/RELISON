%
% Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
% de Madrid, http://ir.ii.uam.es.
%
%  This Source Code Form is subject to the terms of the Mozilla Public
%  License, v. 2.0. If a copy of the MPL was not distributed with this
%  file, You can obtain one at http://mozilla.org/MPL/2.0/.
%

function lhn2 = globallhnindex(adjMatrix, numUsers, numEdges, phi)
% GLOBALLHNINDEX Implementation of the global Leicht-Holme-Newman algorithm
%   @args adjMatrix adjacency matrix
%   @args numUsers number of users in the network
%   @args numEdges number of edges in the network
%   @args phi dampening factor
%
%   @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
%   @author Pablo Castells (pablo.castells@uam.es)
%
    D = inv(diag(sum(adjMatrix)));
    lambda1 = max(eig(adjMatrix));
    X = eye(numUsers);
    Y = (phi./lambda1).*adjMatrix;
    
    Z = X - Y;
    lhn2 = 2.*numEdges.*lambda1.*(D*inv(Z)*D);
end