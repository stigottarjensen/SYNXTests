var fs = require('fs');
var https = require('https');
var querystring = require('querystring');

const propfile = fs.readFileSync('./SenderReceiverTestParams.properties', { encoding: 'utf8' });
const pa = propfile.split('\n');

const props = {};

pa.forEach((line => {
  if (!line.startsWith('#')) {
    la = line.split('=');
    props[la[0]] = la[1];
  }
}));


const JSONText = fs.readFileSync(props.inputFilenameJson, { encoding: 'utf8' });

const JSONInput = JSON.parse(JSONText);
const rtw = JSONInput['RTW'];

const synxcat = process.argv.length > 2 ? process.argv[2] : '1';

const options = {
  host: props.httpUrl,
  path: props.path,
  method: 'POST',
  headers: {
    'Synx-Cat': synxcat,
    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
  }
};

if (synxcat === '4') {
  options.headers['Connection'] = 'Keep-Alive';
  options.headers['Keep-Alive'] = 'timeout=1200,max=250';
}

const jsonBody = synxcat === '4' ? { format: "json" } : JSON.parse(JSON.stringify(rtw));
jsonBody['token'] = props[synxcat === '4' ? 'receiver_token' : 'sender_token'];
jsonBody['objectid'] = props[synxcat === '4' ? 'receiver_objectid' : 'sender_objectid'];
const urlBody = querystring.stringify(jsonBody);
options.headers['Content-Length'] = urlBody.length;

const request = https.request(options, (res) => {
  console.log('Response kode: ', res.statusCode);

  let data = '';
  res.on('data', (chunk) => { data += chunk; console.log(data);});
  res.on('close', () => {
    console.log('pækki');
    console.log(data);
  });
});

request.write(urlBody);
request.end();
request.on('error', (err) => {
  console.error(`Encountered an error trying to make a request: ${err.message}`);
});