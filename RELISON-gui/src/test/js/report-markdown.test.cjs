const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const context = vm.createContext({});
vm.runInContext(fs.readFileSync(path.join(__dirname, '../../main/resources/public/report-markdown.js'), 'utf8') + '\nglobalThis.md = ReportMarkdown;', context);
const md = context.md;

test('inline formatting preserves surrounding text and reselects the formatted words for toggling', () => {
    for (const [command, before, after] of [['bold','**','**'],['italic','*','*'],['underline','<u>','</u>'],['strike','~~','~~']]) {
        const result = md.format('Before selected after', 7, 15, command);
        assert.equal(result.text, 'Before ' + before + 'selected' + after + ' after');
        assert.equal(result.text.slice(result.start, result.end), 'selected');
        assert.equal(md.format(result.text, result.start, result.end, command).text, 'Before selected after');
    }
});

test('emphasis keeps selected whitespace outside Markdown delimiters and inserts editable starter text', () => {
    const result = md.format('one two ', 3, 8, 'bold');
    assert.equal(result.text, 'one **two** ');
    assert.equal(result.text.slice(result.start, result.end), 'two');
    const empty = md.format('', 0, 0, 'bold');
    assert.equal(empty.text, '**bold text**');
    assert.equal(empty.text.slice(empty.start, empty.end), 'bold text');
});

test('colour, font and size controls only insert supported styles', () => {
    for (const [command, value, property] of [['colour','#ff5500','color:#ff5500'],['font','Times New Roman','font-family:Times New Roman'],['size','24','font-size:24px']]) {
        assert.equal(md.format('hello', 0, 5, command, value).text, `<span style="${property}">hello</span>`);
    }
    for (const [command,value] of [['colour','red;position:fixed'],['font','url(https://example.com)'],['size','999'],['size','16;display:none']])
        assert.equal(md.format('hello', 0, 5, command, value).text, 'hello');
});

test('heading controls affect complete selected lines without changing adjacent paragraphs', () => {
    const text = 'Before\nFirst\nSecond\nAfter';
    const result = md.format(text, 8, 19, 'heading', '2');
    assert.equal(result.text, 'Before\n## First\n## Second\nAfter');
    assert.equal(md.format(result.text, result.start, result.end, 'heading', '0').text, text);
    assert.equal(md.format('## Existing', 5, 5, 'heading', '3').text, '### Existing');
});

test('lists and quotations operate on all selected lines and retain blank lines', () => {
    const source = 'First\n\nSecond';
    const list = md.format(source, 0, source.length, 'bullet');
    assert.equal(list.text, '- First\n\n- Second');
    assert.equal(md.format(list.text, list.start, list.end, 'bullet').text, source);
    assert.equal(md.format('- First\n- Second', 0, 16, 'numbered').text, '1. First\n2. Second');
    const quote = md.format(source, 0, source.length, 'quote');
    assert.equal(quote.text, '> First\n> \n> Second');
    assert.equal(md.format(quote.text, quote.start, quote.end, 'quote').text, source);
});

test('code formatting handles literal backticks and fenced multiline content', async () => {
    const { marked } = await import('marked');
    const inline = md.format('a`b', 0, 3, 'code');
    assert.equal(marked.parse(inline.text).trim(), '<p><code>a`b</code></p>');
    const block = md.format('line\n```\nline', 0, 13, 'code');
    assert.match(block.text, /^````\n/);
    assert.match(marked.parse(block.text), /<pre><code>line\n```\nline/);
});

test('link insertion preserves the label and selects the URL for replacement', () => {
    const result = md.format('Read the paper', 9, 14, 'link');
    assert.equal(result.text, 'Read the [paper](https://example.com)');
    assert.equal(result.text.slice(result.start, result.end), 'https://example.com');
});

test('text is escaped if the Markdown dependencies are unavailable', () => {
    assert.equal(md.render('<script>alert(1)</script>'), '<p>&lt;script&gt;alert(1)&lt;/script&gt;</p>');
});
