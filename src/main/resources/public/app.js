const form = document.getElementById('uploadForm');
const result = document.getElementById('result');

form.addEventListener('submit', async (event) => {
    event.preventDefault();

    const fileInput = document.getElementById('file');
    const documentIdInput = document.getElementById('documentId');
    const file = fileInput.files[0];

    if (!file) {
        result.className = 'message error';
        result.textContent = 'Please select a file to upload.';
        return;
    }

    if (!documentIdInput.value.trim()?.length) {
        result.className = 'message error';
        result.textContent = 'Please enter document id';
        return;
    }

    const formData = new FormData();
    formData.append('documentId', documentIdInput.value);
    formData.append('file', file);

    result.className = 'message info';
    result.textContent = 'Uploading...';

    try {
        const response = await fetch('/document-process/', {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            throw new Error('Request failed');
        }

    } catch (error) {
        console.error('Error uploading file:', error);
    }
});

const webSocket = new WebSocket("ws://" +
    location.hostname +
    ":" +
    location.port +
    "/ws/notifier/file");
webSocket.onmessage = function (msg) {
    const data = JSON.parse(msg.data)
    console.log('Received message: ', data)
    if (data.topic.includes("success:")) {
        result.className = 'message success';
        result.textContent = data.message || 'Document processed successfully.';
    }

    if (data.topic.includes("failure:")) {
        result.className = 'message error';
        result.textContent = data.message || 'Upload failed. Check the server logs.';
    }

};
webSocket.onclose = function () { console.log("WebSocket connection closed") };