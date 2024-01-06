urls = {}
for line in io.lines("urls.txt") do
   table.insert(urls, line)
end

request = function()
   url = urls[math.random(#urls)]
   return wrk.format(nil, url)
end
