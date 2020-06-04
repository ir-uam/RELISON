function ht = hittingTime(adjMatrix, numUsers, numEdges)
% COMMUTETIME Finds the hitting time of an adjacency matrix
%   @args adjMatrix adjacency matrix
%   @args numUsers number of users in the network
%   @args numEdges number of edges in the network
%
%   @author Javier Sanz-Cruzado
%   @author Pablo Castells
%
    D = diag(sum(adjMatrix));
    L = D - adjMatrix;
    pinvL = pinv(L);
    
    ht = zeros(numUsers, numUsers);
    for i = 1:numUsers
        for j = 1:numUsers
            for k = 1:numUsers
                ht(i,j) = ht(i,j) + D(k,k).*(pinvL(i,k)-pinvL(i,j)-pinvL(j,k)+pinvL(j,j))
            end
        end
    end
end

