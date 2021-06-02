%
% Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
% de Madrid, http://ir.ii.uam.es.
%
%  This Source Code Form is subject to the terms of the Mozilla Public
%  License, v. 2.0. If a copy of the MPL was not distributed with this
%  file, You can obtain one at http://mozilla.org/MPL/2.0/.
%

function ct = commuteTime(adjMatrix, numUsers, numEdges)
% COMMUTETIME Finds the commute time of an adjacency matrix
%   @args adjMatrix adjacency matrix
%   @args numUsers number of users in the network
%   @args numEdges number of edges in the network
%
%   @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
%   @author Pablo Castells (pablo.castells@uam.es)
%

    D = diag(sum(adjMatrix));
    L = D - adjMatrix;
    pinvL = pinv(L);
    
    ct = zeros(numUsers, numUsers);
    for i = 1:numUsers
        for j = 1:numUsers
            ct(i,j) = numEdges.*(pinvL(i,i) + pinvL(j,j) - 2.*pinvL(i,j));
        end
    end
end

