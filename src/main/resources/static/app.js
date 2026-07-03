const form = document.querySelector("#analysisForm");
const input = document.querySelector("#txHashInput");
const message = document.querySelector("#analysisMessage");
const loadHighRiskBtn = document.querySelector("#loadHighRiskBtn");
const refreshHighRiskBtn = document.querySelector("#refreshHighRiskBtn");
const pageInput = document.querySelector("#pageInput");
const sizeInput = document.querySelector("#sizeInput");

const state = {
    lastHash: "",
};

function setMessage(text) {
    if (!text) {
        message.classList.add("hidden");
        message.textContent = "";
        return;
    }
    message.textContent = text;
    message.classList.remove("hidden");
}

function valueOrDash(value) {
    return value === null || value === undefined || value === "" ? "-" : value;
}

function numberText(value, digits = 8) {
    if (value === null || value === undefined || Number.isNaN(Number(value))) {
        return "-";
    }
    return Number(value).toFixed(digits);
}

function riskClass(level) {
    const normalized = String(level || "").toLowerCase();
    if (normalized === "high") return "risk-high";
    if (normalized === "medium") return "risk-medium";
    if (normalized === "low") return "risk-low";
    return "";
}

function renderKv(targetId, rows) {
    const target = document.querySelector(targetId);
    target.innerHTML = rows.map(([key, value]) => `
        <dt>${key}</dt>
        <dd>${valueOrDash(value)}</dd>
    `).join("");
}

async function requestJson(url) {
    const response = await fetch(url);
    const payload = await response.json();
    if (!response.ok || payload.code !== 200) {
        throw new Error(payload.message || `Request failed: ${response.status}`);
    }
    return payload.data;
}

function renderAnalysis(data) {
    const transaction = data.transaction || {};
    const features = data.features || {};
    const risk = data.risk || {};

    const riskLevel = document.querySelector("#riskLevel");
    riskLevel.textContent = valueOrDash(risk.riskLevel);
    riskLevel.className = riskClass(risk.riskLevel);

    document.querySelector("#anomalyScore").textContent = numberText(risk.anomalyScore);
    document.querySelector("#blockId").textContent = valueOrDash(transaction.blockId);
    document.querySelector("#feeRate").textContent = `${numberText(transaction.feeRate, 4)} sat/vB`;

    renderKv("#transactionDetails", [
        ["Hash", transaction.txHash],
        ["Time", transaction.txTime],
        ["Size", transaction.size],
        ["Weight", transaction.weight],
        ["Inputs", transaction.inputCount],
        ["Outputs", transaction.outputCount],
        ["Input Total", transaction.inputTotal],
        ["Output Total", transaction.outputTotal],
        ["Fee", transaction.fee],
    ]);

    renderKv("#featureDetails", [
        ["Input/Output", numberText(features.inputOutputRatio)],
        ["Amount Entropy", numberText(features.amountEntropy)],
        ["Round Ratio", numberText(features.roundAmountRatio)],
        ["Dust Ratio", numberText(features.dustOutputRatio)],
        ["Version", features.featureVersion],
    ]);

    document.querySelector("#riskReason").textContent = risk.reason || "No risk result.";
}

async function loadAnalysis(txHash) {
    setMessage("");
    const data = await requestJson(`/api/transactions/${encodeURIComponent(txHash)}/analysis`);
    renderAnalysis(data);
    state.lastHash = txHash;
}

async function loadHighRisk() {
    const page = Math.max(1, Number(pageInput.value || 1));
    const size = Math.min(100, Math.max(1, Number(sizeInput.value || 10)));
    pageInput.value = page;
    sizeInput.value = size;

    const data = await requestJson(`/api/transactions/risk/high?page=${page}&size=${size}`);
    const table = document.querySelector("#highRiskTable");
    const records = data.records || [];

    table.innerHTML = records.map(row => `
        <tr>
            <td class="hash-cell">${row.txHash}</td>
            <td>${numberText(row.anomalyScore)}</td>
            <td class="${riskClass(row.riskLevel)}">${row.riskLevel}</td>
            <td>${valueOrDash(row.reason)}</td>
        </tr>
    `).join("");

    document.querySelector("#highRiskMeta").textContent =
        `Page ${data.page}, size ${data.size}, total ${data.total}`;
}

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const txHash = input.value.trim();
    if (!txHash) {
        setMessage("Transaction hash is required.");
        return;
    }

    try {
        await loadAnalysis(txHash);
    } catch (error) {
        setMessage(error.message);
    }
});

loadHighRiskBtn.addEventListener("click", async () => {
    try {
        await loadHighRisk();
    } catch (error) {
        document.querySelector("#highRiskMeta").textContent = error.message;
    }
});

refreshHighRiskBtn.addEventListener("click", async () => {
    try {
        if (state.lastHash) {
            await loadAnalysis(state.lastHash);
        }
        await loadHighRisk();
    } catch (error) {
        setMessage(error.message);
    }
});

loadHighRisk().catch((error) => {
    document.querySelector("#highRiskMeta").textContent = error.message;
});
