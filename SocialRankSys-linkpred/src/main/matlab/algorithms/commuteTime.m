function ct = commuteTime(adjMatrix, numUsers, numEdges)
% COMMUTETIME Finds the commute time of an adjacency matrix
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
    
    ct = zeros(numUsers, numUsers);
    for i = 1:numUsers
        for j = 1:numUsers
            ct(i,j) = numEdges.*(pinvL(i,i) + pinvL(j,j) - 2.*pinvL(i,j));
        end
    end
end

