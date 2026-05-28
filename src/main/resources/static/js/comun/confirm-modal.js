/**
 * efConfirm — Bootstrap Modal de confirmación alineado con el design system.
 *
 * Uso:
 *   const ok = await efConfirm({ title, message, confirmText, variant });
 *   if (!ok) return;
 *
 * Variantes: 'danger' (default) | 'warning' | 'primary'
 */
(function () {
  const MODAL_ID = 'ef-confirm-modal';

  const VARIANT_MAP = {
    danger:  { icon: '&#9888;',  iconClass: 'ef-confirm-icon--danger',  btnClass: 'btn-danger'         },
    warning: { icon: '&#9888;',  iconClass: 'ef-confirm-icon--warning', btnClass: 'btn-warning text-dark' },
    primary: { icon: '&#8505;',  iconClass: 'ef-confirm-icon--primary', btnClass: 'btn-primary'         },
  };

  function buildModal() {
    const el = document.createElement('div');
    el.id = MODAL_ID;
    el.className = 'modal fade';
    el.tabIndex = -1;
    el.setAttribute('aria-modal', 'true');
    el.setAttribute('role', 'dialog');
    el.innerHTML = `
      <div class="modal-dialog modal-dialog-centered ef-confirm-dialog">
        <div class="modal-content ef-confirm-content">
          <div class="modal-body ef-confirm-body">
            <div id="ef-confirm-icon" class="ef-confirm-icon" aria-hidden="true"></div>
            <h5 id="ef-confirm-title" class="ef-confirm-title"></h5>
            <p  id="ef-confirm-message" class="ef-confirm-message"></p>
          </div>
          <div class="modal-footer ef-confirm-footer">
            <button id="ef-confirm-cancel" type="button" class="btn btn-ghost-secondary" data-bs-dismiss="modal">
              Cancelar
            </button>
            <button id="ef-confirm-ok" type="button" class="btn">Confirmar</button>
          </div>
        </div>
      </div>`;
    document.body.appendChild(el);
    return el;
  }

  function getOrCreateModal() {
    return document.getElementById(MODAL_ID) ?? buildModal();
  }

  /**
   * @param {object}  opts
   * @param {string}  opts.title
   * @param {string}  opts.message
   * @param {string}  [opts.confirmText='Confirmar']
   * @param {string}  [opts.cancelText='Cancelar']
   * @param {'danger'|'warning'|'primary'} [opts.variant='danger']
   * @returns {Promise<boolean>}
   */
  window.efConfirm = function ({
    title       = '¿Confirmar acción?',
    message     = '',
    confirmText = 'Confirmar',
    cancelText  = 'Cancelar',
    variant     = 'danger',
  } = {}) {
    const el     = getOrCreateModal();
    const config = VARIANT_MAP[variant] ?? VARIANT_MAP.danger;

    el.querySelector('#ef-confirm-icon').innerHTML    = config.icon;
    el.querySelector('#ef-confirm-icon').className    = `ef-confirm-icon ${config.iconClass}`;
    el.querySelector('#ef-confirm-title').textContent = title;
    el.querySelector('#ef-confirm-message').innerHTML = message;

    const btnOk     = el.querySelector('#ef-confirm-ok');
    const btnCancel = el.querySelector('#ef-confirm-cancel');

    btnOk.textContent = confirmText;
    btnOk.className   = `btn ${config.btnClass}`;
    btnCancel.textContent = cancelText;

    const bsModal = bootstrap.Modal.getOrCreateInstance(el, { backdrop: 'static', keyboard: false });

    return new Promise(resolve => {
      function cleanup() {
        btnOk.removeEventListener('click', onOk);
        el.removeEventListener('hide.bs.modal', onCancel);
      }
      function onOk() {
        cleanup();
        bsModal.hide();
        resolve(true);
      }
      function onCancel() {
        cleanup();
        resolve(false);
      }

      btnOk.addEventListener('click', onOk);
      el.addEventListener('hide.bs.modal', onCancel, { once: true });

      bsModal.show();
    });
  };
})();

/**
 * efAlert — Modal informativo (sin cancelar) alineado con el design system.
 *
 * Uso:
 *   await efAlert({ title, message, variant });
 */
(function () {
  const MODAL_ID = 'ef-alert-modal';

  const VARIANT_MAP = {
    danger:  { icon: '&#9888;',  iconClass: 'ef-confirm-icon--danger',  btnClass: 'btn-danger'            },
    warning: { icon: '&#9888;',  iconClass: 'ef-confirm-icon--warning', btnClass: 'btn-warning text-dark'  },
    primary: { icon: '&#8505;',  iconClass: 'ef-confirm-icon--primary', btnClass: 'btn-primary'            },
  };

  function buildModal() {
    const el = document.createElement('div');
    el.id = MODAL_ID;
    el.className = 'modal fade';
    el.tabIndex = -1;
    el.setAttribute('aria-modal', 'true');
    el.setAttribute('role', 'dialog');
    el.innerHTML = `
      <div class="modal-dialog modal-dialog-centered ef-confirm-dialog">
        <div class="modal-content ef-confirm-content">
          <div class="modal-body ef-confirm-body">
            <div id="ef-alert-icon" class="ef-confirm-icon" aria-hidden="true"></div>
            <h5 id="ef-alert-title" class="ef-confirm-title"></h5>
            <p  id="ef-alert-message" class="ef-confirm-message"></p>
          </div>
          <div class="modal-footer ef-confirm-footer">
            <button id="ef-alert-ok" type="button" class="btn" data-bs-dismiss="modal">Aceptar</button>
          </div>
        </div>
      </div>`;
    document.body.appendChild(el);
    return el;
  }

  function getOrCreateModal() {
    return document.getElementById(MODAL_ID) ?? buildModal();
  }

  window.efAlert = function ({
    title   = 'Aviso',
    message = '',
    variant = 'warning',
  } = {}) {
    const el     = getOrCreateModal();
    const config = VARIANT_MAP[variant] ?? VARIANT_MAP.warning;

    el.querySelector('#ef-alert-icon').innerHTML    = config.icon;
    el.querySelector('#ef-alert-icon').className    = `ef-confirm-icon ${config.iconClass}`;
    el.querySelector('#ef-alert-title').textContent = title;
    el.querySelector('#ef-alert-message').innerHTML = message;

    const btnOk = el.querySelector('#ef-alert-ok');
    btnOk.className = `btn ${config.btnClass}`;

    const bsModal = bootstrap.Modal.getOrCreateInstance(el);

    return new Promise(resolve => {
      el.addEventListener('hide.bs.modal', () => resolve(), { once: true });
      bsModal.show();
    });
  };
})();
