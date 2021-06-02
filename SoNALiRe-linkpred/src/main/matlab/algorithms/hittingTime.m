%
% Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
% de Madrid, http://ir.ii.uam.es.
%
%  This Source Code Form is subject to the terms of the Mozilla Public
%  License, v. 2.0. If a copy of the MPL was not distributed with this
%  file, You can obtain one at http://mozilla.org/MPL/2.0/.
%

function ht = hittingTime(adjMatrix, numUsers, numEdges)
% HITTINGTIME Finds the hitting time of an adjacency matrix
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
    
    ht = zeros(numUsers, numUsers);
    
    % First, we find sum(D(k,k)*pinvL(i,j))
    ht = ht - numEdges.*pinvL;
    % Next, we obtain the matrix with columns equal to pinvL(j,j)*|E|
    for j = 1:numUsers
        ht(:,j) = ht(:,j) + numEdges.*pinvL(j,j);
    end
    % Now, we just have to find the two remaining values
    tL = transpose(pinvL);
    tL = D*tL;
    v = sum(tL);
    
    for i = 1:numUsers
        for j = 1:numUsers
            ht(i,j) = ht(i,j)+v(i)-v(j);
        end
    end
end

