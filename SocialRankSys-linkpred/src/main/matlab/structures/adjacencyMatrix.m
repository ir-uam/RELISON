function adj = adjacencyMatrix(index,train)
% ADJACENCYMATRIX Finds the directed adjacency matrix of a graph
%   @args index the users
%   @args train the training data containing the links
%
%   @author Javier Sanz-Cruzado
%   @author Pablo Castells
%
    adj = zeros(length(index), length(index));
    for i = 1:size(train,1)
        uIdx = find(index == train(i,1));
        vIdx = find(index == train(i,2));
        adj(uIdx,vIdx) = train(i,3);
    end

end